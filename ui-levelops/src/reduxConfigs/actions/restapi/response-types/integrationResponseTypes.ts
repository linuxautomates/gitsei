export interface IntegrationDetailResponse {
  application: string;
  authentication: string;
  created_at: number;
  description: string;
  id: string;
  metadata: { [key: string]: string | string[] | boolean | number | Object };
  name: string;
  satellite: boolean;
  status: string;
  tags: string[];
  updated_at: number;
  url: string;
}
