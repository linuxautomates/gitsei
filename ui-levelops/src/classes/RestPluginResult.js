export class RestPluginResult {
  constructor(data = null) {
    this.summary = {
      items: [],
      details: {}
    };
    this.tableData = [];
    this.rawReport = undefined;
    this.metadata = {};
    this.type = undefined;
    if (data !== null) {
      this.rawReport = data;
      this.type = data.tool;
      this.metadata = {
        labels: data.labels,
        created_at_epoch: data.created_at_epoch,
        product_ids: data.product_ids
      };
      if (data.tool === "report_ms_tmt") {
        const results = data.results.results;
        this.summary = {
          details: {
            created_at: results.created_at,
            threat_model_name: results.threat_model_name,
            owner: results.owner,
            contributors: results.contributors,
            reviewer: results.reviewer
          }
        };
      }
    }
  }
}
