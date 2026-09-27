
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
} from "recharts";

function PulseChart({ data }) {
  return (
    <div className="pulse-chart">
      <h2>Live Camera Color Signal</h2>

      <p className="chart-status">
  Baseline-corrected camera color signal
</p>

      {data.length === 0 ? (
        <p>Waiting for face detection and camera samples...</p>
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={data}>
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="#263449"
            />

            <XAxis
              dataKey="time"
              stroke="#94a3b8"
              tick={false}
            />

            <YAxis
              domain={["auto", "auto"]}
              reversed
              stroke="#94a3b8"
              tick={{ fill: "#94a3b8" }}
            />

            <Tooltip />

            <Line
              type="monotone"
              dataKey="pulse"
              stroke="#00f5a0"
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

export default PulseChart;