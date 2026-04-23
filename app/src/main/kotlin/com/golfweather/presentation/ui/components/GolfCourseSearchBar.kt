package com.golfweather.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.golfweather.data.model.GolfCourse

/**
 * 골프장 실시간 자동완성 검색바
 *
 * [버그 수정] DropdownMenu → ExposedDropdownMenuBox
 *  - 기존 Box + DropdownMenu 구조에서는 DropdownMenu가 Compose 팝업 레이어에서
 *    부모 Box의 너비를 상속하지 못해 드롭다운이 보이지 않거나 위치가 어긋남.
 *  - ExposedDropdownMenuBox + menuAnchor()를 사용하면 TextField 너비에
 *    정확히 맞춰 드롭다운이 렌더링됨.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GolfCourseSearchBar(
    query: String,
    results: List<GolfCourse>,
    isSearching: Boolean,
    isDropdownExpanded: Boolean,
    onQueryChanged: (String) -> Unit,
    onCourseSelected: (GolfCourse) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(
        expanded = isDropdownExpanded && results.isNotEmpty(),
        onExpandedChange = { expanded -> if (!expanded) onDismiss() },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            // menuAnchor: 이 TextField를 드롭다운 위치 기준점으로 등록
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text("골프장 검색") },
            placeholder = { Text("골프장 이름을 입력하세요 (2글자 이상)") },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = "검색")
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "지우기")
                    }
                }
            },
            singleLine = true
        )

        // ExposedDropdownMenu: TextField와 동일 너비로 자동 앵커링
        ExposedDropdownMenu(
            expanded = isDropdownExpanded && results.isNotEmpty(),
            onDismissRequest = onDismiss
        ) {
            results.forEach { course ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (course.address.isNotEmpty()) {
                                Text(
                                    text = course.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.GolfCourse,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { onCourseSelected(course) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
