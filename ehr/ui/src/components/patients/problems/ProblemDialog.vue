<script lang="ts">
import { defineComponent, PropType, reactive } from "vue";
import { TableData } from "@/components/patients/action-steps/ActionSteps.vue";

export type FormModel = {
	id: string,
	name: string,
	basedOn: string,
	startDate: string,
	closedDate?: string,
	goals: number,
	actionSteps: number,
	clinicalStatus: string,
	code: string,
	category: string
};

export default defineComponent({
	name: "ProblemDialog",
	props: {
		visible: {
			type: Boolean,
			default: false
		},
		problem: {
			type: Object as PropType<TableData>,
			default: undefined
		}
	},
	emits: ["close"],
	setup(props, { emit }) {
		const formModel = reactive<FormModel>({
			id: "",
			name: "",
			basedOn: "",
			startDate: "",
			closedDate: "",
			category: "",
			goals: 0,
			actionSteps: 0,
			clinicalStatus: "",
			code: ""
		});

		const onDialogOpen = () => {
			Object.assign(formModel, props.problem);
		};

		const onDialogClose = () => {
			emit("close");
		};

		return {
			formModel,
			onDialogOpen,
			onDialogClose
		};
	}
});
</script>

<template>
	<el-dialog
		:model-value="visible"
		title="Problem Details"
		:width="700"
		append-to-body
		destroy-on-close
		custom-class="problem-dialog"
		@close="onDialogClose"
		@opened="onDialogOpen"
	>
		<el-form
			:model="formModel"
			label-width="155px"
			label-position="left"
			size="mini"
			class="problem-form"
		>
			<el-form-item label="Problem">
				{{ formModel.name }}
			</el-form-item>
			<el-form-item label="Category">
				{{ formModel.category }}
			</el-form-item>
			<el-form-item label="Code">
				{{ formModel.code }}
			</el-form-item>
			<el-form-item label="Base on">
				{{ formModel.basedOn }}
			</el-form-item>
			<el-form-item label="Assessment Date">
				{{ $filters.formatDateTime(formModel.startDate) }}
			</el-form-item>
			<el-form-item
				v-if="formModel.closedDate"
				label="Closed Date"
			>
				{{ $filters.formatDateTime(formModel.closedDate) }}
			</el-form-item>
		</el-form>
		<template #footer>
			<el-button
				plain
				round
				type="primary"
				size="mini"
				@click="$emit('close')"
			>
				Close
			</el-button>
		</template>
	</el-dialog>
</template>
