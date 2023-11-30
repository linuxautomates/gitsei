export type DevRawStatsDataType = {
  org_user_id: number;
  full_name: string;
  ou_attributes: Record<string, string>;
  raw_stats: Array<Record<string, number | string>>;
};

export type DevRawStatsDataTypeNew = {
  org_user_id: number;
  full_name: string;
  ou_attributes: Record<string, string>;
  raw_stats: Record<string, number>;
  section_responses?: Array<DevRawStatsDataType>;
  custom_fields?: Record<string, string>;
};

