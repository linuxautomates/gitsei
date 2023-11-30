export interface JiraSelfManagedDTO {
  identifier: string;
  name: string;
  description: string;
  tags?: {
    [key: string]: string;
  };
  jiraURL: string;
  username: string;
  apiKey: string;
  jqlQuery: string;
}
