export const PROPEL_FETCH = "PROPEL_FETCH";
export const PROPEL_NEW = "PROPEL_NEW";

export const propelFetch = id => ({
  type: PROPEL_FETCH,
  id: id
});

export const propelNew = type => ({
  type: PROPEL_NEW,
  trigger_type: type
});
