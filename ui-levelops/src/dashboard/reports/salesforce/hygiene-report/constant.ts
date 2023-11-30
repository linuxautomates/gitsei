export const SALESFORCE_HYGIENE_REPORT = {
  unit: "tickets",
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets"
    }
  ],
  stacked: false,
  sortBy: "total_tickets",
  chartProps: {
    barGap: 0,
    margin: { top: 20, right: 5, left: 5, bottom: 50 }
  }
};

export const SALESFORCE_HYGIENE_TYPES = ["IDLE", "POOR_DESCRIPTION", "NO_CONTACT", "MISSED_RESOLUTION_TIME"];
