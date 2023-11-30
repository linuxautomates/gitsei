export const getOuIntegrationsData = (sections: any) => {
  return sections.reduce((acc: any, next: any) => {
    const integrationId = Object.keys(next.integrations || {})?.[0];
    const integrationData: any = Object.values(next.integrations || {})?.[0];
    const integration = {
      ...(integrationData || {}),
      id: integrationId,
      dynamic_user_definition: next?.["dynamic_user_definition"] || {},
      users: next?.["users"] || []
    };
    return [...acc, integration];
  }, []);
};


