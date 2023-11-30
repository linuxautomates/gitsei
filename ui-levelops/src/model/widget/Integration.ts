export interface Integration {
  id: string;
  name: string;
  descriptions: string;
  application: string;
  authentication: string;
  // timestamp value
  created_at: number;
  metadata: any;
  satellite: boolean;
  status: string;
  tags: string[];
  // timestamp value
  updated_at: number;
  url: string;
}
