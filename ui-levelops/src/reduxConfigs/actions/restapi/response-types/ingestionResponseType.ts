export enum IngestionStatus {
  HEALTHY = "healthy",
  WARNING = "warning",
  FAILED = "failed",
  UNKNOWN = "unknown"
}

export interface IngestionIntegrationStatus {
  status: IngestionStatus;
  last_ingested_activity_from?: number;
  last_ingested_activity_to?: number;
}

export interface IngestionIntegrationLog {
  id: string;
  status: string;
  description: string;
  attempt_count: number;
  from: number;
  to: number;
  created_at: number;
  updated_at: number;
  elapsed: number;
}
