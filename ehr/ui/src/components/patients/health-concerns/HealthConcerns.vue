<script lang="ts">
import { computed, defineComponent, ref } from "vue";
import { Concern } from "@/types";
import { ConcernsModule } from "@/store/modules/concerns";
import HealthConcernsTable from "@/components/patients/health-concerns/HealthConcernsTable.vue";

export type TableData = {
	name: string,
	status: string,
	createdAt: string,
	category: string,
	basedOn: string,
	actions: string,
	id: string
}

export default defineComponent({
	name: "HealthConcerns",
	components: {
		HealthConcernsTable
	},
	setup() {
		const isRequestLoading = ref<boolean>(false);
		const concerns = computed<Concern[]>(() => ConcernsModule.concerns);
		const tableData = computed<TableData[]>(() =>
			concerns.value.map((concern: Concern) => ({
				name: concern.name,
				status: concern.status,
				createdAt: concern.createdAt,
				category: concern.category,
				basedOn: concern.basedOn,
				actions: concern.actions,
				id: concern.id
			}))
		);
		const activeConcerns = computed<TableData[]>(() => tableData.value.filter(t => t.status === "Active"));
		const promotedOrResolvedConcerns = computed<TableData[]>(() => tableData.value.filter(t => t.status === "PromotedOrResolved"));

		return {
			activeConcerns,
			promotedOrResolvedConcerns,
			isRequestLoading
		};
	}
});
</script>

<template>
	<div class="health-concerns">
		<div
			v-loading="isRequestLoading"
		>
			<HealthConcernsTable
				v-if="activeConcerns.length"
				:data="activeConcerns"
				type="ActiveConcerns"
			/>
			<HealthConcernsTable
				v-if="promotedOrResolvedConcerns.length"
				:data="promotedOrResolvedConcerns"
				type="PromotedOrResolvedConcerns"
				class="promoted-concerns"
				title="Promoted Or Resolved Health Concerns"
			/>
			<div
				v-if="!isRequestLoading && !(promotedOrResolvedConcerns.length || activeConcerns.length)"
				class="no-request-data"
			>
				<h2>No Health Concerns Yet</h2>
				<el-button
					plain
					round
					type="primary"
					size="mini"
				>
					Add Health Concern
				</el-button>
			</div>
		</div>
	</div>
</template>

<style lang="scss" scoped>
@import "~@/assets/scss/abstracts/variables";

.no-request-data {
	height: 240px;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: flex-start;
	background-color: $global-background;

	h2 {
		color: $whisper;
		font-size: $global-xxxlarge-font-size;
		font-weight: $global-font-weight-normal;
		margin-bottom: 50px;
	}
}

.promoted-concerns {
	margin-top: 30px;
}
</style>
