interface ProjectsListResponse {
  projectsInfo: {
    name: string;
    key: string;
  };
  id: string;
  lastUpdatedAt: string;
  selected: boolean;
}

interface ProjectsListProps {
  selectProjectsCount: string;
}
