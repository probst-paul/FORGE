# Roadmap

## Not Yet Implemented

- Additional event detectors beyond first-hour breach
- Analytics feature calculation beyond placeholder models
- Full multi-position and scale-in/scale-out trade lifecycle behavior
- Realistic market, limit, stop, and slippage execution simulation
- Additional market event evaluation against market data
- Partial fills and advanced order execution simulation

## Planned Design Areas

FORGE is in early architectural development. Implemented paths include SCID-to-PostgreSQL ingestion, rollover-aware catalog availability, exact tick-based price storage, event statistics, a basic backtest simulation path, and concurrent processing for independent event-statistics/backtest contract windows.

The `engine/backtest` and `engine/eventstatistics` packages produce run result objects, while `reporting/backtest` and `reporting/eventstatistics` prepare those results for display and export-oriented workflows. Richer execution simulation, deeper replay behavior, and scale-in/scale-out trade lifecycle behavior are planned design areas.
