package com.devmarkabrasaldo.PataGilid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devmarkabrasaldo.PataGilid.R
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FilterPill(
    text: String,
    iconResId: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) Color(0xFF1A73E8) else Color(0xFFF1F3F4),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF3C4043),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFF3C4043),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun DismissableBadge(
    text: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF1A73E8),
        modifier = Modifier.clickable { onDismiss() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Clear Filter",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun IslandGroupFilterBar(
    allCount: Int,
    isAllSelected: Boolean,
    selectedIslandGroup: IslandGroup?,
    onResetFilters: () -> Unit,
    onSelectIslandGroup: (IslandGroup?) -> Unit,
    extraBadges: @Composable RowScope.() -> Unit = {}
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    
    Surface(
        color = Color(0xFFF8F9FA),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPill(
                text = "All (${numberFormat.format(allCount)})",
                iconResId = R.drawable.philippines_icon,
                isSelected = isAllSelected,
                onClick = onResetFilters
            )
            
            IslandGroup.entries.forEach { island ->
                val iconRes = when (island) {
                    IslandGroup.LUZON -> R.drawable.luzon_icon
                    IslandGroup.VISAYAS -> R.drawable.visayas_icon
                    IslandGroup.MINDANAO -> R.drawable.mindanao_icon
                }
                FilterPill(
                    text = island.displayName,
                    iconResId = iconRes,
                    isSelected = selectedIslandGroup == island,
                    onClick = {
                        onSelectIslandGroup(if (selectedIslandGroup == island) null else island)
                    }
                )
            }
            
            extraBadges()
        }
    }
}

@Composable
fun CountBanner(
    filteredCount: Int,
    totalCount: Int,
    noun: String,
    showDivider: Boolean = true
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Showing ${numberFormat.format(filteredCount)} of ${numberFormat.format(totalCount)} $noun",
                color = Color(0xFF5F6368),
                fontSize = 13.sp
            )
        }
        if (showDivider) {
            HorizontalDivider(color = Color(0xFFE8EAED), thickness = 1.dp)
        }
    }
}

@Composable
fun TopBarSearchFilterAction(
    isSearchVisible: Boolean,
    onToggleSearch: () -> Unit,
    isMenuExpanded: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF1A73E8),
            modifier = Modifier
                .size(36.dp)
                .clickable { onToggleSearch() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1A73E8),
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onToggleMenu(true) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { onToggleMenu(false) },
                modifier = Modifier.background(Color.White),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                menuContent()
            }
        }
    }
}

@Composable
fun SearchFilterToolbar(
    isSearchVisible: Boolean,
    onToggleSearch: () -> Unit,
    isMenuExpanded: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onAdd: (() -> Unit)? = null,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (onAdd != null) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A73E8),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onAdd() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Surface(
                shape = CircleShape,
                color = Color(0xFF1A73E8),
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onToggleSearch() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Box {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A73E8),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onToggleMenu(true) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Sort and Filter",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { onToggleMenu(false) },
                    modifier = Modifier.background(Color.White),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    menuContent()
                }
            }
        }
    }
}

@Composable
fun FloatingSearchFilterToolbar(
    isSearchVisible: Boolean,
    onToggleSearch: () -> Unit,
    isMenuExpanded: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A73E8),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onToggleSearch() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1A73E8),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onToggleMenu(true) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter and Sort Menu",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { onToggleMenu(false) },
                        modifier = Modifier.background(Color.White),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp
                    ) {
                        menuContent()
                    }
                }
            }
        }
    }
}


data class SortMenuItem(
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun SortOrderMenuSection(
    items: List<SortMenuItem>
) {
    Text(
        text = "Sort Order",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF5F6368)
    )
    
    items.forEach { item ->
        DropdownMenuItem(
            text = {
                Text(
                    text = item.label,
                    color = Color(0xFF202124),
                    fontSize = 15.sp,
                    fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            leadingIcon = {
                if (item.isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            },
            onClick = item.onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RegionFilterMenuSection(
    availableRegions: List<String>,
    selectedRegion: String?,
    onSelectRegion: (String?) -> Unit
) {
    Text(
        text = "Filter by Region",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF5F6368)
    )
    
    DropdownMenuItem(
        text = {
            Text(
                text = "All Regions",
                color = Color(0xFF202124),
                fontSize = 15.sp,
                fontWeight = if (selectedRegion == null) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        leadingIcon = {
            if (selectedRegion == null) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(20.dp))
            }
        },
        onClick = { onSelectRegion(null) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    )

    availableRegions.forEach { region ->
        DropdownMenuItem(
            text = {
                Text(
                    text = region,
                    color = Color(0xFF202124),
                    fontSize = 15.sp,
                    fontWeight = if (selectedRegion == region) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            leadingIcon = {
                if (selectedRegion == region) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            },
            onClick = { onSelectRegion(region) },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CustomFilterMenuSection(
    title: String,
    items: List<SortMenuItem>
) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF5F6368)
    )
    
    items.forEach { item ->
        DropdownMenuItem(
            text = {
                Text(
                    text = item.label,
                    color = Color(0xFF202124),
                    fontSize = 15.sp,
                    fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            leadingIcon = {
                if (item.isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            },
            onClick = item.onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
