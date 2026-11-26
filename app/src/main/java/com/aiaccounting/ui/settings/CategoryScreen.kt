package com.aiaccounting.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.domain.TxType
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.HairLine
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.PageBg
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub

/** 可选图标，够覆盖日常记账就行。 */
private val EMOJI_CHOICES = listOf(
    "🍜", "☕", "🛒", "🚌", "🚗", "🏠", "💊", "📱", "🎮", "🎁",
    "👶", "🐶", "💄", "✈️", "🎓", "📚", "🛍️", "💰", "💵", "🎉",
    "🧾", "🏥", "🚿", "💡",
)

/** 分类页需要的全部数据（含子分类），界面保持无状态，方便测试和快照。 */
data class CategoryPageState(
    val expense: List<CategoryEntity> = emptyList(),
    val income: List<CategoryEntity> = emptyList(),
    val rules: List<Pair<String, String>> = emptyList(),
    val message: String? = null,
)

/** 正在编辑/新建的分类。editing 为 null 表示新建；parentId 非空表示建的是子分类。 */
private data class CategoryDraft(
    val editing: CategoryEntity?,
    val kind: TxType,
    val parentId: Long? = null,
    val emoji: String = editing?.emoji ?: "🧾",
    val name: String = editing?.name ?: "",
)

@Composable
fun CategoryContent(
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    merchantRules: List<Pair<String, String>>,
    message: String?,
    onAddCategory: (name: String, emoji: String, kind: TxType, parentId: Long?) -> Unit,
    onUpdateCategory: (id: Long, name: String, emoji: String) -> Unit,
    onDeleteCategory: (id: Long) -> Unit,
    onDeleteMerchantRule: (pattern: String) -> Unit,
) {
    var draft by remember { mutableStateOf<CategoryDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    // 点开的一级分类：底部弹出它的子分类
    var openedParent by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "轻触分类可编辑 / 删除；带子分类的点开可以管理子分类",
            fontSize = 11.sp,
            color = TextSub,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        CategorySection(
            title = "支出管理",
            categories = expenseCategories,
            allCategories = expenseCategories,
            onPick = { openedParent = it },
            onAdd = { draft = CategoryDraft(editing = null, kind = TxType.EXPENSE) },
            addTag = "addExpenseCategory",
        )

        Spacer(Modifier.height(16.dp))

        CategorySection(
            title = "收入管理",
            categories = incomeCategories,
            allCategories = incomeCategories,
            onPick = { openedParent = it },
            onAdd = { draft = CategoryDraft(editing = null, kind = TxType.INCOME) },
            addTag = "addIncomeCategory",
        )

        if (merchantRules.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "已学习的商户分类",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White),
            ) {
                merchantRules.forEachIndexed { index, (merchant, category) ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(1.dp)
                                .background(HairLine),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = merchant, fontSize = 14.sp, color = TextMain)
                            Text(
                                text = "自动归到「$category」",
                                fontSize = 11.sp,
                                color = TextSub,
                            )
                        }
                        Text(
                            text = "删除",
                            fontSize = 12.sp,
                            color = AlertRed,
                            modifier = Modifier
                                .testTag("rule_$merchant")
                                .clip(RoundedCornerShape(50))
                                .clickable { onDeleteMerchantRule(merchant) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "删掉之后，这个商户下次改分类时会重新学习。",
                fontSize = 11.sp,
                color = TextFaint,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = if (message.contains("不能") || message.contains("还有") || message.contains("先")) {
                    AlertRed
                } else {
                    IncomeGreen
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    // 子分类面板
    val parent = openedParent
    if (parent != null) {
        val all = expenseCategories + incomeCategories
        SubCategorySheet(
            parent = parent,
            children = all.filter { it.parentId == parent.id },
            onDismiss = { openedParent = null },
            onEditParent = {
                draft = CategoryDraft(editing = parent, kind = runCatching { TxType.valueOf(parent.kind) }.getOrDefault(TxType.EXPENSE))
                openedParent = null
            },
            onAddChild = {
                draft = CategoryDraft(editing = null, kind = runCatching { TxType.valueOf(parent.kind) }.getOrDefault(TxType.EXPENSE), parentId = parent.id)
            },
            onPickChild = { child ->
                draft = CategoryDraft(editing = child, kind = runCatching { TxType.valueOf(parent.kind) }.getOrDefault(TxType.EXPENSE), parentId = parent.id)
            },
        )
    }

    val current = draft
    if (current != null) {
        val all = expenseCategories + incomeCategories
        CategoryEditSheet(
            draft = current,
            parentName = current.parentId?.let { id -> all.firstOrNull { it.id == id }?.name },
            onDismiss = { draft = null },
            onSave = { name, emoji ->
                val editing = current.editing
                if (editing == null) {
                    onAddCategory(name, emoji, current.kind, current.parentId)
                } else {
                    onUpdateCategory(editing.id, name, emoji)
                }
                draft = null
            },
            onDelete = {
                current.editing?.let { pendingDelete = it }
                draft = null
            },
        )
    }

    val deleting = pendingDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除「${deleting.name}」？") },
            text = {
                Text(
                    if (deleting.parentId == null) {
                        "这个一级分类下如果还有子分类或记录，会拦下来。"
                    } else {
                        "如果这个子分类下还有记录，会提示你先改掉。"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(deleting.id)
                    pendingDelete = null
                }) { Text("删除", color = AlertRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CategorySection(
    title: String,
    categories: List<CategoryEntity>,
    allCategories: List<CategoryEntity>,
    onPick: (CategoryEntity) -> Unit,
    onAdd: () -> Unit,
    addTag: String,
) {
    val parents = categories.filter { it.parentId == null }
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextMain,
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 8.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val cells = parents.map { it as CategoryEntity? } + listOf(null)
        cells.chunked(5).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (item == null) {
                            CategoryCell(
                                emoji = "＋",
                                label = "添加",
                                modifier = Modifier.testTag(addTag),
                                onClick = onAdd,
                            )
                        } else {
                            val childCount = allCategories.count { it.parentId == item.id }
                            CategoryCell(
                                emoji = item.emoji,
                                label = item.name,
                                badge = if (childCount > 0) "$childCount" else null,
                                modifier = Modifier.testTag("category_${item.id}"),
                                onClick = { onPick(item) },
                            )
                        }
                    }
                }
                repeat(5 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCell(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandBlueSofter),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandBlue)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(text = badge, fontSize = 9.sp, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSub,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** 一级分类的子分类面板：跟小米钱包一样，点进去看子分类、加子分类。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubCategorySheet(
    parent: CategoryEntity,
    children: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onEditParent: () -> Unit,
    onAddChild: () -> Unit,
    onPickChild: (CategoryEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandBlueSofter),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = parent.emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = parent.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Text(
                        text = if (children.isEmpty()) "还没有子分类" else "${children.size} 个子分类",
                        fontSize = 11.sp,
                        color = TextSub,
                    )
                }
                Text(
                    text = "编辑",
                    fontSize = 12.sp,
                    color = BrandBlue,
                    modifier = Modifier
                        .testTag("editParentCategory")
                        .clip(RoundedCornerShape(50))
                        .clickable { onEditParent() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairLine),
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "子分类",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "点一下子分类可以改名、换图标或删除",
                fontSize = 11.sp,
                color = TextFaint,
            )

            Spacer(Modifier.height(10.dp))
            val cells = children.map { it as CategoryEntity? } + listOf(null)
            cells.chunked(5).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (item == null) {
                                CategoryCell(
                                    emoji = "＋",
                                    label = "添加",
                                    modifier = Modifier.testTag("addSubCategory"),
                                    onClick = onAddChild,
                                )
                            } else {
                                CategoryCell(
                                    emoji = item.emoji,
                                    label = item.name,
                                    modifier = Modifier.testTag("sub_${item.id}"),
                                    onClick = { onPickChild(item) },
                                )
                            }
                        }
                    }
                    repeat(5 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "记账时选了这个一级分类，会再让你选具体是哪个子分类。",
                fontSize = 11.sp,
                color = TextFaint,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditSheet(
    draft: CategoryDraft,
    parentName: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(draft) { mutableStateOf(draft.name) }
    var emoji by remember(draft) { mutableStateOf(draft.emoji) }
    val editing = draft.editing
    val isChild = draft.parentId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = when {
                    editing != null -> "编辑分类"
                    isChild -> "新建子分类（$parentName）"
                    else -> "新建分类"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            if (editing?.isBuiltIn == true) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "内置分类：可以改名换图标，但不能删除",
                    fontSize = 11.sp,
                    color = TextFaint,
                )
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 8) name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("categoryNameField"),
            )

            Spacer(Modifier.height(14.dp))
            Text(text = "图标", fontSize = 12.sp, color = TextSub)
            Spacer(Modifier.height(8.dp))
            EMOJI_CHOICES.chunked(8).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEach { candidate ->
                        val selected = candidate == emoji
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (selected) BrandBlueSofter else Color.Transparent)
                                .then(
                                    if (selected) {
                                        Modifier.border(1.5.dp, BrandBlue, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { emoji = candidate },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = candidate, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(BrandBlue)
                        .clickable(enabled = name.isNotBlank()) { onSave(name, emoji) }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("保存", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                if (editing != null && !editing.isBuiltIn) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(PageBg)
                            .clickable { onDelete() }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("删除", fontSize = 15.sp, color = AlertRed)
                    }
                }
            }
        }
    }
}
