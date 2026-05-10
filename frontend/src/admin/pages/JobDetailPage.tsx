import { useParams } from 'react-router-dom';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';

export default function JobDetailPage() {
  const { jobId } = useParams<{ jobId: string }>();

  return (
    <div>
      <PageHeader
        title="Job Detail"
        description={`Job ${jobId ?? '—'}`}
      />
      <Card>
        <p className="text-sm text-gray-500">
          Job detail with SubJobs table and retry actions will be implemented in a later phase.
        </p>
      </Card>
    </div>
  );
}
