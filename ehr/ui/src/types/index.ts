export type Patient = {
	address: string | null,
	age: number | null,
	dob: string | null,
	gender: string | null,
	id: string | null,
	name: string | null
	language: string | null,
	phones: Phone[],
	emails: Email[],
	employmentStatus: string | null,
	race: string | null,
	ethnicity: string | null,
	education: string | null,
	maritalStatus: string | null,
	insurances: string[]
};

export type Email = {
	email: string,
	use: string
};

export type Phone = {
	phone: string,
	use: string
};

export type User = {
	id: string | null,
	name: string | null,
	userType: string | null
};

export type ContextResponse = {
	patient: Patient,
	user: User
};

export type Task = {
	comments: Comment[],
	createdAt: string,
	errors: string[],
	id: string,
	lastModified: string | null,
	name: string,
	organization: Organization | null,
	outcome: string | null,
	priority: "ASAP" | "Routine" | "Urgent" | null,
	procedures: Procedure[]
	serviceRequest: ServiceRequest,
	status: TaskStatus,
	statusReason: string | null
};

export type Organization = {
	errors: string[],
	name: string,
	id: string,
	type: "CBO" | "CBRO"
};

export type Occurrence = {
	start?: string | null,
	end: string
}

export type ServiceRequestGoal = {
	display: string,
	id: string
}

export type ServiceRequestConsent = {
	display: string,
	id: string
}

export type Coding = {
	code: string,
	display: string
}

export type ServiceRequestCondition = {
	display: string,
	id: string
};

export type Procedure = {
	display: string,
	id: string
}

export type Period = {
	start: string,
	end?: string
}

export type ServiceRequest = {
	category: Coding,
	code: Coding,
	conditions: ServiceRequestCondition[],
	consent: ServiceRequestConsent,
	errors: string[],
	goals: ServiceRequestGoal[],
	id: string,
	occurrence: Occurrence
};

export type newTaskPayload = {
	category: string,
	conditionIds: string[],
	consent: boolean,
	comment: string,
	goalIds: string[],
	performerId: string,
	code: string,
	name: string,
	occurrence: Occurrence | string
};

export type updateTaskPayload = {
	comment?: string,
	status: TaskStatus | null,
	id: string
}

export type TaskStatus = "Accepted" | "Cancelled" | "Completed" | "Draft" | "Entered In Error" | "Failed" | "In Progress" | "Null" | "On Hold" | "Ready" | "Received" | "Rejected" | "Requested"

export type Comment = {
	author: {
		display: string,
		id: string,
		resourceType: string
	},
	text: string,
	time: string
};

export type Concern = {
	name: string,
	createdAt: string,
	category: string
	id: string,
	basedOn: string,
	actions: string,
	status: string
};

export type Assessment = {
	name: string,
	createdAt: string,
	concerns: string
	id: string,
	questions: string[],
	actions: string
	status: "Past" | "Planned"
};

export type Problem = {
	id: string,
	name: string,
	basedOn: string,
	onsetPeriod: Period,
	goals: number,
	actionSteps: number,
	clinicalStatus: string,
	code: string,
	category: string
};

export type Goal = {
	name: string,
	problems: string[],
	addedBy: string,
	startDate: string,
	endDate: string,
	targets: string[],
	comments: Comment[],
	category: Coding,
	code: Coding,
	status: "active" | "completed"
};
