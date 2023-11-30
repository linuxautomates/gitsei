export const buildQueryParam = (accountId: string, orgIdentifier: string, projectIdentifier: string) => ({
  routingId: accountId,
  accountIdentifier: accountId,
  projectIdentifier: projectIdentifier,
  orgIdentifier: orgIdentifier,
})