export const isDashboardTimerangeEnabled = (dashboardMetadata: any): boolean => {
  if (dashboardMetadata.hasOwnProperty('dashboard_time_range')) {
    return dashboardMetadata.dashboard_time_range;
  }
  return dashboardMetadata.hasOwnProperty('dashboard_time_range_filter');
}