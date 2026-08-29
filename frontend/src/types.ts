export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type Reference = { id: string; name: string }

export type Assignment = {
  id: string
  employee: Reference
  training: Reference
  trainingVersionId: string
  trainingVersion: number
  origin: string
  assignedAt: string
  assignedDate: string
  dueDate?: string | null
  status: string
  priority: string
  recertification: boolean
  batchId?: string | null
}

export type LearningVideo = {
  id: string
  title: string
  description?: string
  order: number
  durationSeconds: number
  required: boolean
  positionSeconds: number
  percentageWatched: number
  completed: boolean
}

export type LearningModule = {
  id: string
  title: string
  description?: string
  order: number
  videos: LearningVideo[]
  questionnaire?: { id: string; title: string; available: boolean } | null
}

export type AssignmentDetail = Assignment & {
  learningPath: {
    assignmentId: string
    trainingVersionId: string
    assignmentStatus: string
    modules: LearningModule[]
    assessment: { required: boolean; available: boolean; summary?: string }
  }
  resumePoint?: {
    assignmentId: string
    moduleId: string
    videoId: string
    positionSeconds: number
  } | null
}

export type Qualification = {
  id: string
  employee: { id: string; name: string; registration: string }
  activity: Reference
  status: string
  calculatedAt: string
  nextExpirationDate?: string | null
  blockingReasons: Array<{
    type: string
    trainingId?: string
    trainingName?: string
    expirationDate?: string
    assignmentStatus?: string
  }>
  disclaimer: string
}

export type Certificate = {
  id: string
  completionId: string
  type: string
  validationCode: string
  issuedDate: string
  issuedAt: string
  status: string
  revocationReason?: string | null
  generationNumber: number
}

export type QrCodeData = {
  id: string
  employeeId: string
  token: string
  verificationUrl: string
  status: string
  generatedAt: string
  revokedAt?: string | null
  revocationReason?: string | null
}

export type Notification = {
  id: string
  type: string
  title: string
  message: string
  relatedEntityType?: string | null
  relatedEntityId?: string | null
  createdAt: string
  readAt?: string | null
  archivedAt?: string | null
}

export type Employee = {
  id: string
  name: string
  registration: string
  email: string
  status: string
  photoUrl?: string | null
  job: Reference
  sector: Reference
  unit: Reference
}

export type Training = {
  id: string
  name: string
  code: string
  description: string
  category: string
  regulatoryStandard: boolean
  status: string
}

export type TrainingVersion = {
  id: string
  trainingId: string
  versionNumber: number
  workloadMinutes: number
  validityType: 'DAYS' | 'MONTHS' | 'INDEFINITE'
  validityValue?: number | null
  passingScore: number
  maxAttempts?: number | null
  retryIntervalMinutes: number
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  publishedAt?: string | null
  trainingNameSnapshot?: string
  trainingCodeSnapshot?: string
}

export type TrainingModule = {
  id: string
  trainingVersionId: string
  title: string
  description?: string | null
  order: number
  status: 'ACTIVE' | 'INACTIVE'
}

export type TrainingVideo = {
  id: string
  moduleId: string
  title: string
  description?: string | null
  order: number
  durationSeconds: number
  storageObjectKey?: string | null
  required: boolean
  status: 'ACTIVE' | 'INACTIVE'
  fileId?: string | null
}

export type TrainingQuestionnaire = {
  id: string
  moduleId: string
  title: string
  passingScore: number
  maxAttempts?: number | null
  retryIntervalMinutes: number
  shuffleQuestions: boolean
  status: 'ACTIVE' | 'INACTIVE'
}

export type TrainingQuestion = {
  id: string
  questionnaireId: string
  statement: string
  order: number
  status: 'ACTIVE' | 'INACTIVE'
}

export type TrainingAnswerOption = {
  id: string
  questionId: string
  text: string
  correct: boolean
  order: number
  status: 'ACTIVE' | 'INACTIVE'
}

export type ContentSummary = {
  versionId: string
  activeModules: number
  activeRequiredVideos: number
  activeQuestionnaires: number
  activeQuestions: number
  publishable: boolean
  violations: string[]
}

export type UploadResponse = {
  uploadId: string
  fileId: string
  purpose: string
  state: string
  method?: string | null
  uploadUrl?: string | null
  objectKey?: string | null
  expiresAt: string
  requiredHeaders: Record<string, string>
}

export type Activity = {
  id: string
  name: string
  description: string
  status: string
}

export type Expiration = {
  completionId: string
  employeeId: string
  trainingId: string
  completionDate: string
  expirationDate?: string | null
  status: string
}

export type PersonalDashboard = {
	continueTraining?: {
		assignmentId: string
		trainingName: string
		progressPercentage: number
		resumeAt?: { videoId: string; positionSeconds: number } | null
	} | null
  counts: {
    pending: number
    inProgress: number
    expiringSoon: number
    expired: number
    completed: number
		availableActivities: number
		blockedActivities: number
  }
	pendingTrainings: DashboardTrainingSummary[]
	expiringTrainings: DashboardTrainingSummary[]
	blockedActivities: Array<{
		activityId: string
		activityName: string
		status: string
		blockingTrainings: string[]
	}>
}

export type DashboardTrainingSummary = {
	assignmentId: string
	trainingId: string
	trainingName: string
	status: string
	dueDate?: string | null
	progressPercentage: number
}

export type AdminDashboard = {
  activeEmployees: number
  registeredTrainings: number
  assignedTrainings: number
  notStarted: number
  inProgress: number
  completed: number
  failed: number
  expired: number
  expiringIn30Days: number
	employeesWithPendingItems: number
	employeesWithBlockedActivities: number
  generatedAt: string
}

export type TrainingDashboardItem = {
	trainingId: string
	trainingName: string
	trainingCode: string
	assigned: number
	notStarted: number
	inProgress: number
	latestAssessmentApproved: number
	latestAssessmentFailed: number
	completed: number
	expired: number
	completionRate: number
	averageLatestAssessment: number
	averageCompletionHours: number
}

export type ActivityDashboardItem = {
	activityId: string
	activityName: string
	relatedJobs: number
	requirements: number
	availableEmployees: number
	expiringEmployees: number
	blockedEmployees: number
	mainBlockingTrainings: string[]
}

export type EmployeeDashboardItem = {
	employeeId: string
	employeeName: string
	registration: string
	unitId: string
	unitName: string
	sectorId: string
	sectorName: string
	jobId: string
	jobName: string
	mandatoryTrainings: number
	optionalTrainings: number
	averageProgress: number
	averageLatestAssessment: number
	completions: number
	expirations: number
	availableActivities: number
	blockedActivities: number
}

export type Questionnaire = {
  id: string
  title: string
  shuffleQuestions: boolean
  questions: Array<{
    id: string
    statement: string
    options: Array<{ id: string; text: string }>
  }>
}

export type AssessmentAvailability = {
  assignmentId: string
  questionnaireId: string
  passingScore: number
  attemptsUsed: number
  maxAttempts?: number | null
  attemptsRemaining?: number | null
  available: boolean
  nextAttemptAt?: string | null
}

export type AssessmentResult = {
  attemptId: string
  attemptNumber: number
  score: number
  passingScore: number
  result: string
  assignmentStatus: string
  completedAt?: string | null
  nextAttemptAt?: string | null
}
