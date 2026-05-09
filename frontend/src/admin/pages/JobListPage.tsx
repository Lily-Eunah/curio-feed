import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';

export default function JobListPage() {
  return (
    <div>
      <PageHeader title="Jobs" description="AI generation job monitoring" />
      <EmptyState
        title="Job dashboard"
        message="Job dashboard will be implemented in a later phase."
      />
    </div>
  );
}
