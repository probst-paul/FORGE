# Roadmap

## Not Yet Implemented

- Additional event detectors beyond first-hour breach and price crossover
- Additional derived analytics beyond session ranges, volume/activity measurements, and first-hour breach occurrences
- Full multi-position and scale-in/scale-out trade lifecycle behavior
- Realistic market, limit, stop, and slippage execution simulation
- Partial fills and advanced order execution simulation
- CSV/PDF report export from saved or in-memory report models
- Volume-based rollover selection and comparison workflows

## Planned Design Areas

FORGE is in early architectural development. Implemented paths include SCID-to-PostgreSQL ingestion, overlap-aware import modes, rollover-aware catalog availability, exact tick-based price storage, event statistics with summary/detail views, a basic backtest simulation path, and concurrent processing for independent event-statistics/backtest contract windows.

The `engine/backtest` and `engine/eventstatistics` packages produce run result objects, while `reporting/backtest` and `reporting/eventstatistics` prepare those results for display and export-oriented workflows. Richer execution simulation, deeper replay behavior, and scale-in/scale-out trade lifecycle behavior are planned design areas.
