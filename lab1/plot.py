import csv
from pathlib import Path

import matplotlib.pyplot as plt

THREAD_COUNTS = (1, 2, 4, 6, 8, 10, 12)

def plot_benchmarks(
    csv_path = "benchmark-results.csv",
    output_path = "plot.png",
) -> None:
    latest = {}

    with Path(csv_path).open(newline="", encoding="utf-8") as csv_file:
        reader = csv.DictReader(csv_file)
        required_columns = {"collector", "threads", "ops_per_second"}
        if reader.fieldnames is None or not required_columns.issubset(reader.fieldnames):
            raise ValueError(
                "CSV must contain collector, threads, and ops_per_second columns"
            )

        for row in reader:
            threads = int(row["threads"])
            if threads in THREAD_COUNTS:
                # Later rows overwrite earlier ones: the CSV is an append-only log.
                latest[(row["collector"], threads)] = float(row["ops_per_second"])

    if not latest:
        raise ValueError("CSV contains no benchmark results for the requested threads")

    collectors = sorted({collector for collector, _ in latest})

    fig, ax = plt.subplots(figsize=(12, 7))
    for collector in collectors:
        points = [
            (threads, latest[(collector, threads)])
            for threads in THREAD_COUNTS
            if (collector, threads) in latest
        ]
        ax.plot(
            [threads for threads, _ in points],
            [ops for _, ops in points],
            marker="o",
            linewidth=2,
            label=collector,
        )

    ax.set_title("Metrics collector benchmark")
    ax.set_xlabel("Threads")
    ax.set_ylabel("Operations per second")
    ax.set_xticks(THREAD_COUNTS)
    ax.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
    ax.set_yscale("log", base=10)
    ax.grid(True, alpha=0.3)
    ax.legend()
    fig.tight_layout()
    fig.savefig(output_path, dpi=150)
    plt.close(fig)


if __name__ == "__main__":
    plot_benchmarks()
