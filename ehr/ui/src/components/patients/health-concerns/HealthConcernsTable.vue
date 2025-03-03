<script lang="ts">
import { defineComponent, PropType, ref } from "vue";
import EditConcernDialog from "@/components/patients/health-concerns/EditConcernDialog.vue";
import { TableData } from "@/components/patients/health-concerns/HealthConcerns.vue";
import ActionButton from "@/components/patients/ActionButton.vue";

export default defineComponent({
	name: "HealthConcernsTable",
	components: {
		EditConcernDialog,
		ActionButton
	},
	props: {
		data: {
			type: Array as PropType<TableData[]>,
			required: true
		},
		title: {
			type: String,
			default: "Active Health Concerns"
		},
		type: {
			type: String,
			required: true
		}
	},
	setup() {
		const editConcernDialogVisible = ref<boolean>(false);
		const editConcern = ref<TableData>();
		const onConcernClick = (row: TableData) => {
			editConcernDialogVisible.value = true;
			editConcern.value = row;
		};

		return {
			editConcernDialogVisible,
			onConcernClick,
			editConcern
		};
	}
});
</script>

<template>
	<div>
		<div
			class="table-wrapper"
		>
			<div class="title">
				<h3>
					{{ title }}
				</h3>
			</div>
			<el-table :data="data">
				<el-table-column
					label="Health Concern"
				>
					<template #default="scope">
						<el-button
							type="text"
							@click="onConcernClick(scope.row)"
						>
							{{ scope.row.name }}
						</el-button>
					</template>
				</el-table-column>
				<el-table-column
					label="Category"
				>
					<template #default="scope">
						{{ scope.row.category }}
					</template>
				</el-table-column>
				<el-table-column
					label="Based On"
				>
					<template #default="scope">
						{{ scope.row.basedOn }}
					</template>
				</el-table-column>
				<el-table-column
					label="Assessment Date"
				>
					<template #default="scope">
						{{ $filters.formatDateTime(scope.row.createdAt) }}
					</template>
				</el-table-column>
				<el-table-column
					v-if="type === 'ActiveConcerns'"
					label="Actions"
					width="350"
				>
					<ActionButton
						icon-class="icon-promote"
						label="Promote to Problem"
					/>
					<ActionButton
						icon-class="icon-resolved"
						label="Mark as Resolved"
					/>
					<ActionButton
						icon-class="icon-remove"
						label="Remove"
					/>
				</el-table-column>
				<el-table-column
					v-if="type === 'PromotedOrResolvedConcerns'"
					label="Status"
				>
					<template #default="scope">
						{{ scope.row.actions }}
					</template>
				</el-table-column>
				<el-table-column
					v-if="type === 'PromotedOrResolvedConcerns'"
					label="Promotion/Resolution Date"
				>
					May 5, 2021, 10:00 AM
				</el-table-column>
			</el-table>
		</div>

		<EditConcernDialog
			:visible="editConcernDialogVisible"
			:concern="editConcern"
			@close="editConcernDialogVisible = false"
		/>
	</div>
</template>

<style lang="scss" scoped>
@import "~@/assets/scss/abstracts/variables";
@import "~@/assets/scss/abstracts/mixins";

.title {
	margin: 10px 20px 0;

	h3 {
		font-weight: $global-font-weight-medium;
		margin: 0;
	}
}

.table-wrapper {
	background-color: $global-background;
	border-radius: 5px;
	border: 1px solid $global-base-border-color;
	padding: 10px 20px;
	min-height: 130px;
}

::v-deep(.el-table--enable-row-hover) .el-table__body tr:hover {
	box-shadow: 0 2px 5px 0 rgba(51, 51, 51, 0.25);
	border-radius: 5px;
}
</style>
