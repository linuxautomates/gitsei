export enum PriorityTypes {
  HIGHEST = "HIGHEST",
  HIGH = "HIGH",
  MEDIUM = "MEDIUM",
  LOW = "LOW",
  LOWEST = "LOWEST"
}

export const PriorityOrderMapping = {
  [PriorityTypes.HIGHEST]: 1,
  [PriorityTypes.HIGH]: 2,
  [PriorityTypes.MEDIUM]: 3,
  [PriorityTypes.LOW]: 4,
  [PriorityTypes.LOWEST]: 5
};
export const priorityMappping = Object.values(PriorityTypes);

export const STATIC_PRIORTIES = {
  "1": PriorityTypes.HIGHEST,
  "2": PriorityTypes.HIGH,
  "3": PriorityTypes.MEDIUM,
  "4": PriorityTypes.LOW,
  "5": PriorityTypes.LOWEST
};
// TODO: replace staticPriorties by STATIC_PRIORTIES;
export const staticPriorties = STATIC_PRIORTIES;
