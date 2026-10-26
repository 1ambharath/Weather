"""
Thailand Weather Intelligence — Backend Ingestion & Alert Service
Provides high-performance RSS normalization, headline deduplication,
geospatial incident matching, and TMD radar metadata dispatch.
"""

from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional
import time
import math

app = FastAPI(
    title="Thailand Weather Intelligence Backend",
    version="1.0.0",
    description="Ingestion and geospatial incident normalization service for Thailand meteorological data."
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class CoordinateRequest(BaseModel):
    latitude: float
    longitude: float
    radius_km: float = 25.0

class AlertResponse(BaseModel):
    id: str
    title: str
    title_thai: Optional[str]
    description: str
    severity: str
    source: str
    issued_at: int
    expires_at: int
    affected_provinces: List[str]
    instruction: Optional[str]

class ArticleResponse(BaseModel):
    id: str
    title: str
    summary: str
    publisher: str
    published_at: int
    url: str
    category: str
    province: Optional[str]

# Haversine distance
def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

# Mock / In-memory normalized alert storage
SAMPLE_ALERTS = [
    {
        "id": "tmd_warn_2026_01",
        "title": "TMD Warning No. 4: Heavy to Very Heavy Rain & Strong Winds in Upper Thailand",
        "title_thai": "ประกาศเตือนภัย กรมอุตุนิยมวิทยา: ฝนตกหนักถึงหนักมากและคลื่นลมแรง",
        "description": "Monsoon trough lies across Central, Eastern and Lower Northern regions of Thailand.",
        "severity": "Warning",
        "source": "Thai Meteorological Department (TMD)",
        "issued_at": int(time.time() * 1000) - 7200000,
        "expires_at": int(time.time() * 1000) + 86400000 * 2,
        "affected_provinces": ["Chiang Mai", "Chiang Rai", "Phuket", "Krabi", "Surat Thani", "Chon Buri"],
        "instruction": "Residents in lowlands and near steep foothills should prepare for flash floods and runoff."
    },
    {
        "id": "ddpm_flood_chao_phraya",
        "title": "DDPM Warning: High River Levels and Spillway Release from Chao Phraya Dam",
        "title_thai": "แจ้งเตือนระดับน้ำล้นตลิ่ง แม่น้ำเจ้าพระยา",
        "description": "Controlled discharge rate elevated. Water levels downstream in Sing Buri, Ang Thong, Ayutthaya rising 20-30 cm.",
        "severity": "Watch",
        "source": "DDPM Thailand",
        "issued_at": int(time.time() * 1000) - 3600000,
        "expires_at": int(time.time() * 1000) + 86400000,
        "affected_provinces": ["Ayutthaya", "Nonthaburi", "Pathum Thani", "Bangkok"],
        "instruction": "Move electrical equipment and livestock to higher ground."
    }
]

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "Thailand Weather Intelligence Backend",
        "timestamp": int(time.time())
    }

@app.get("/api/alerts", response_model=List[AlertResponse])
def get_alerts():
    """Retrieve normalized official alerts from TMD and DDPM."""
    return SAMPLE_ALERTS

@app.post("/api/alerts/match")
def match_alerts(req: CoordinateRequest):
    """Matches active severe weather warnings and incident zones against user coordinates."""
    matched = []
    # Reference coordinates for provinces
    province_coords = {
        "Bangkok": (13.7563, 100.5018),
        "Chiang Mai": (18.7883, 98.9853),
        "Phuket": (7.8804, 98.3923),
        "Surat Thani": (9.1382, 99.3217),
        "Ayutthaya": (14.3532, 100.5684)
    }

    for alert in SAMPLE_ALERTS:
        is_affected = False
        for prov in alert["affected_provinces"]:
            if prov in province_coords:
                p_lat, p_lon = province_coords[prov]
                dist = haversine_km(req.latitude, req.longitude, p_lat, p_lon)
                if dist <= req.radius_km + 100.0: // Province buffer
                    is_affected = True
                    break
        if is_affected:
            matched.append(alert)

    return {
        "matched_count": len(matched),
        "alerts": matched
    }

@app.get("/api/radar")
def get_radar_stations():
    """Returns TMD Doppler radar status and real-time imagery URLs."""
    from data_sources import RADAR_STATIONS
    return RADAR_STATIONS

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
