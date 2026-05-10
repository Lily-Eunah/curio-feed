import PageHeader from '../components/PageHeader';
import Card from '../components/Card';

export default function DashboardPage() {
  return (
    <div>
      <PageHeader title="Dashboard" description="Overview of CurioFeed system status" />
      <Card>
        <p className="text-sm text-gray-500">Dashboard overview will be implemented in a later phase.</p>
      </Card>
    </div>
  );
}
