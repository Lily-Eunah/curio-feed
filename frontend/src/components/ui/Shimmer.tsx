interface Props {
  width: string | number;
  height: string | number;
  radius?: number;
  mb?: number;
}

function Shimmer({ width, height, radius = 6, mb = 0 }: Props) {
  return (
    <div
      style={{
        width,
        height,
        borderRadius: radius,
        background: 'linear-gradient(90deg, #ECEAE4 25%, #F5F3EE 50%, #ECEAE4 75%)',
        backgroundSize: '200% 100%',
        animation: 'shimmer 1.4s infinite',
        marginBottom: mb,
      }}
    />
  );
}

export function SkeletonCard() {
  return (
    <div
      style={{
        background: '#FFFFFF',
        borderRadius: 14,
        padding: 16,
        marginBottom: 10,
        border: '1px solid #F0EDE6',
      }}
    >
      <Shimmer width="60px" height="18px" radius={10} mb={12} />
      <Shimmer width="90%" height="20px" mb={6} />
      <Shimmer width="70%" height="20px" mb={12} />
      <Shimmer width="100%" height="14px" mb={6} />
      <Shimmer width="85%" height="14px" mb={14} />
      <Shimmer width="80px" height="14px" />
    </div>
  );
}

export default Shimmer;
