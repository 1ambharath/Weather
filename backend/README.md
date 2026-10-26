# Thailand Weather Intelligence — Backend Service

## Technical Rationale: Why FastAPI?
1. **Asynchronous I/O Performance**: Python's `asyncio` and `uvicorn` handle concurrent scraping, RSS polling, and geospatial polygon matching with minimal memory consumption (<50 MB RAM vs 300+ MB for a JVM server).
2. **Standardized OpenAPI Documentation**: FastAPI automatically produces interactive Swagger documentation at `/docs` and ReDoc at `/redoc`.
3. **Pydantic Data Validation**: Automatic type validation and serialization for CAP (Common Alerting Protocol) warnings and weather snapshots.
4. **Zero Account Dependency**: Respects user privacy; saved places remain local on the device in Room SQLite without requiring forced server accounts.

## Running Locally
```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
Endpoints:
- `GET http://localhost:8000/health`: Health status.
- `GET http://localhost:8000/api/alerts`: Normalized TMD and DDPM alerts.
- `POST http://localhost:8000/api/alerts/match`: Matches alerts within a geographic radius.
- `GET http://localhost:8000/api/radar`: Real-time radar links and station states.
