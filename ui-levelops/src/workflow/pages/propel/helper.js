export function propelValidation(propel) {
  const { links, nodes } = propel.ui_data || {};

  if (!nodes) {
    return { error: "No nodes found!" };
  }

  if (!links) {
    return { error: "No links found!" };
  }

  if (!(Object.keys(nodes).length > 1)) {
    return { error: "Propel must have minimum 2 nodes" };
  }

  if (!(Object.keys(links).length > 0)) {
    return { error: "Propel must have minimum 1 link" };
  }
  const nodesList = Object.values(nodes).map(node => node.id);
  const linksList = Object.values(links).map(link => ({ to: link.to.nodeId, from: link.from.nodeId }));

  if (nodesList.length > 1 && linksList.length > 0) {
    let valid = true;

    for (let i = 0; i < linksList.length; i++) {
      const { to, from } = linksList[i];
      if (!nodesList.includes(to) || !nodesList.includes(from)) {
        valid = false;
        break;
      }
    }

    if (!valid) {
      return { error: "All links must be connected to node" };
    } else {
      return { error: "" };
    }
  }

  return { error: "Invalid propel. Please upload file containing valid propel" };
}
