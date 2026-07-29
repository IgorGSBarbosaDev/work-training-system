import { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { Role } from './api'
import { homeForRole, useAuth } from './auth'
import { AppShell } from './layout'
import {
  CertificateValidationPage,
  ErrorPage,
  LoginPage,
  RecoveryPage,
  ResetPasswordPage,
} from './pages-auth'
import { EmployeeDashboardPage, OperationsDashboardPage } from './pages-dashboard'
import {
  AssessmentResultPage,
  AssignmentDetailPage,
  CertificateDetailPage,
  MyAssignmentsPage,
  MyCertificatesPage,
  MyQrCodePage,
  MyQualificationsPage,
  NotificationsPage,
  ProfilePage,
  QuestionnairePage,
  TrainingPlayerPage,
} from './pages-employee'
import {
  ActivitiesPage,
  AssignmentsAdminPage,
  CreateActivityPage,
  CreateAssignmentPage,
  CreateEmployeePage,
  CreateTrainingPage,
  EmployeeDetailPage,
  EmployeesPage,
  ExpirationsPage,
  GenericManagementPage,
  OrganizationPage,
  QrVerificationPage,
  QrVerificationResultPage,
  QualificationsManagementPage,
  ReportsPage,
  SettingsPage,
  TrainingDetailPage,
  TrainingsPage,
  TrainingVersionEditorPage,
} from './pages-management'

function ProtectedRoute({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { session } = useAuth()
  const location = useLocation()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (!roles.includes(session.user.role)) {
    return <Navigate to="/erro/403" replace />
  }
  return <>{children}</>
}

function HomeRedirect() {
  const { session } = useAuth()
  return <Navigate to={session ? homeForRole(session.user.role) : '/login'} replace />
}

const employeeRoles: Role[] = ['EMPLOYEE']
const managementRoles: Role[] = ['MANAGER', 'SUPERVISOR']
const adminRoles: Role[] = ['ADMIN']

export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/recuperar-senha" element={<RecoveryPage />} />
      <Route path="/redefinir-senha/:token" element={<ResetPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/validar-certificado" element={<CertificateValidationPage />} />
      <Route path="/erro/:code" element={<ErrorPage />} />

      <Route
        element={
          <ProtectedRoute roles={['ADMIN', 'MANAGER', 'SUPERVISOR', 'EMPLOYEE']}>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route path="/meu/dashboard" element={<ProtectedRoute roles={employeeRoles}><EmployeeDashboardPage /></ProtectedRoute>} />
        <Route path="/meu/atribuicoes" element={<ProtectedRoute roles={employeeRoles}><MyAssignmentsPage /></ProtectedRoute>} />
        <Route path="/meu/atribuicoes/:assignmentId" element={<ProtectedRoute roles={employeeRoles}><AssignmentDetailPage /></ProtectedRoute>} />
        <Route path="/meu/atribuicoes/:assignmentId/videos/:videoId" element={<ProtectedRoute roles={employeeRoles}><TrainingPlayerPage /></ProtectedRoute>} />
        <Route path="/meu/atribuicoes/:assignmentId/questionarios/:questionnaireId" element={<ProtectedRoute roles={employeeRoles}><QuestionnairePage /></ProtectedRoute>} />
        <Route path="/meu/atribuicoes/:assignmentId/resultado" element={<ProtectedRoute roles={employeeRoles}><AssessmentResultPage /></ProtectedRoute>} />
        <Route path="/meu/qualificacoes" element={<ProtectedRoute roles={employeeRoles}><MyQualificationsPage /></ProtectedRoute>} />
        <Route path="/meu/certificados" element={<ProtectedRoute roles={employeeRoles}><MyCertificatesPage /></ProtectedRoute>} />
        <Route path="/meu/certificados/:certificateId" element={<ProtectedRoute roles={employeeRoles}><CertificateDetailPage /></ProtectedRoute>} />
        <Route path="/meu/qr-code" element={<ProtectedRoute roles={employeeRoles}><MyQrCodePage /></ProtectedRoute>} />
        <Route path="/meu/notificacoes" element={<ProtectedRoute roles={employeeRoles}><NotificationsPage /></ProtectedRoute>} />
        <Route path="/meu/perfil" element={<ProtectedRoute roles={employeeRoles}><ProfilePage /></ProtectedRoute>} />

        <Route path="/equipe/dashboard" element={<ProtectedRoute roles={managementRoles}><OperationsDashboardPage team /></ProtectedRoute>} />
        <Route path="/equipe/colaboradores" element={<ProtectedRoute roles={managementRoles}><EmployeesPage team /></ProtectedRoute>} />
        <Route path="/equipe/colaboradores/:employeeId" element={<ProtectedRoute roles={managementRoles}><EmployeeDetailPage team /></ProtectedRoute>} />
        <Route path="/equipe/qualificacoes" element={<ProtectedRoute roles={managementRoles}><QualificationsManagementPage team /></ProtectedRoute>} />
        <Route path="/equipe/verificar-qr" element={<ProtectedRoute roles={managementRoles}><QrVerificationPage /></ProtectedRoute>} />
        <Route path="/equipe/verificar-qr/:token" element={<ProtectedRoute roles={managementRoles}><QrVerificationResultPage /></ProtectedRoute>} />
        <Route path="/equipe/relatorios" element={<ProtectedRoute roles={managementRoles}><ReportsPage team /></ProtectedRoute>} />
        <Route path="/equipe/perfil" element={<ProtectedRoute roles={managementRoles}><ProfilePage /></ProtectedRoute>} />

        <Route path="/admin/dashboard" element={<ProtectedRoute roles={adminRoles}><OperationsDashboardPage /></ProtectedRoute>} />
        <Route path="/admin/colaboradores" element={<ProtectedRoute roles={adminRoles}><EmployeesPage /></ProtectedRoute>} />
        <Route path="/admin/colaboradores/novo" element={<ProtectedRoute roles={adminRoles}><CreateEmployeePage /></ProtectedRoute>} />
        <Route path="/admin/colaboradores/:employeeId" element={<ProtectedRoute roles={adminRoles}><EmployeeDetailPage /></ProtectedRoute>} />
        <Route path="/admin/organizacao" element={<ProtectedRoute roles={adminRoles}><OrganizationPage /></ProtectedRoute>} />
        <Route path="/admin/atividades" element={<ProtectedRoute roles={adminRoles}><ActivitiesPage /></ProtectedRoute>} />
        <Route path="/admin/atividades/nova" element={<ProtectedRoute roles={adminRoles}><CreateActivityPage /></ProtectedRoute>} />
        <Route path="/admin/treinamentos" element={<ProtectedRoute roles={adminRoles}><TrainingsPage /></ProtectedRoute>} />
        <Route path="/admin/treinamentos/novo" element={<ProtectedRoute roles={adminRoles}><CreateTrainingPage /></ProtectedRoute>} />
        <Route path="/admin/treinamentos/:trainingId" element={<ProtectedRoute roles={adminRoles}><TrainingDetailPage /></ProtectedRoute>} />
        <Route path="/admin/treinamentos/:trainingId/versoes/:versionId/editor" element={<ProtectedRoute roles={adminRoles}><TrainingVersionEditorPage /></ProtectedRoute>} />
        <Route path="/admin/atribuicoes" element={<ProtectedRoute roles={adminRoles}><AssignmentsAdminPage /></ProtectedRoute>} />
        <Route path="/admin/atribuicoes/nova" element={<ProtectedRoute roles={adminRoles}><CreateAssignmentPage /></ProtectedRoute>} />
        <Route path="/admin/expiracoes" element={<ProtectedRoute roles={adminRoles}><ExpirationsPage /></ProtectedRoute>} />
        <Route path="/admin/certificados" element={<ProtectedRoute roles={adminRoles}><GenericManagementPage kind="certificates" /></ProtectedRoute>} />
        <Route path="/admin/usuarios" element={<ProtectedRoute roles={adminRoles}><GenericManagementPage kind="users" /></ProtectedRoute>} />
        <Route path="/admin/notificacoes" element={<ProtectedRoute roles={adminRoles}><GenericManagementPage kind="notifications" /></ProtectedRoute>} />
        <Route path="/admin/emails" element={<ProtectedRoute roles={adminRoles}><GenericManagementPage kind="emails" /></ProtectedRoute>} />
        <Route path="/admin/auditoria" element={<ProtectedRoute roles={adminRoles}><GenericManagementPage kind="audit" /></ProtectedRoute>} />
        <Route path="/admin/relatorios" element={<ProtectedRoute roles={adminRoles}><ReportsPage /></ProtectedRoute>} />
        <Route path="/admin/configuracoes" element={<ProtectedRoute roles={adminRoles}><SettingsPage /></ProtectedRoute>} />
        <Route path="/admin/perfil" element={<ProtectedRoute roles={adminRoles}><ProfilePage /></ProtectedRoute>} />
      </Route>

      <Route path="*" element={<Navigate to="/erro/404" replace />} />
    </Routes>
  )
}
