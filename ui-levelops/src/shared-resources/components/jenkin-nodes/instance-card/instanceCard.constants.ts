export const instanceCardDisplayFields = (instanceDetails: any) => [
  {
    label: "Jenkins Version",
    value: instanceDetails?.jenkins_version,
    key: "jenkins_version"
  },
  {
    label: "Plugin Version",
    value: instanceDetails?.plugin_version,
    key: "Plugin_version"
  }
];
