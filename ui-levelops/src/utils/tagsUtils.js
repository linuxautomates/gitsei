export const getTagColor = (value = 0) => {
  if (value <= 50) {
    return "green";
  } else if (value <= 100) {
    return "orange";
  }
  return "red";
};

const LOW = 30;
const HIGH = 70;

export const getTagsStyle = count => {
  if (count < LOW) {
    return { color: "var(--green)", backgroundColor: "var(--green-lighter)" };
  } else if (count > HIGH) {
    return { color: "var(--red)", backgroundColor: "var(--red-lighter)" };
  } else {
    return { color: "var(--yellow)", backgroundColor: "var(--yellow-lighter)" };
  }
};
