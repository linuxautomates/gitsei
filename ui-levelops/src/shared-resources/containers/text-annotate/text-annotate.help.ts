export const setMarkBackground = (item: any, color: string) => {
  if (!item) {
    return {};
  }

  if (typeof item === "object" && item.hasOwnProperty("props")) {
    return {
      ...item,
      props: {
        ...item.props,
        style: {
          ...item.props.style,
          backgroundColor: color
        }
      }
    };
  }

  return {};
};
