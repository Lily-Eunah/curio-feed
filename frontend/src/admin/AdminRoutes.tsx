import { Routes, Route, Navigate } from 'react-router-dom';
import AdminShell from './layout/AdminShell';
import DashboardPage from './pages/DashboardPage';
import ArticleListPage from './pages/ArticleListPage';
import ArticleCreatePage from './pages/ArticleCreatePage';
import ArticleDetailPage from './pages/ArticleDetailPage';
import GenerationStatusPage from './pages/GenerationStatusPage';
import JobListPage from './pages/JobListPage';
import JobDetailPage from './pages/JobDetailPage';
import CategoryPage from './pages/CategoryPage';

export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminShell />}>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="articles" element={<ArticleListPage />} />
        <Route path="articles/new" element={<ArticleCreatePage />} />
        <Route path="articles/:articleId/status" element={<GenerationStatusPage />} />
        <Route path="articles/:id" element={<ArticleDetailPage />} />
        <Route path="jobs" element={<JobListPage />} />
        <Route path="jobs/:jobId" element={<JobDetailPage />} />
        <Route path="categories" element={<CategoryPage />} />
      </Route>
    </Routes>
  );
}
