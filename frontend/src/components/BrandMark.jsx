export function BrandMark() {
    return (
        <div
            style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '12px',
                marginBottom: '32px',
            }}
        >
            <span
                style={{
                    fontFamily: "'Grand Hotel', cursive",
                    fontSize: '48px',
                    lineHeight: 1,
                }}
            >
                <span style={{ color: 'var(--text-primary)' }}>Friends</span>
                <span className="text-[var(--accent)]">Hub</span>
            </span>
        </div>
    );
}
