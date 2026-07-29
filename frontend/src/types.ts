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
  counts: {
    pending: number
    inProgress: number
    expiringSoon: number
    expired: number
    completed: number
  }
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
  generatedAt: string
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
