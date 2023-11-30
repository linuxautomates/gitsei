const getStylePosition = (element, tooltip, position) => {
  switch (position) {
    case 'top':
      return {
        top: element.top - tooltip.height - 5,
        left: (element.left + (element.width / 2)) - (tooltip.width / 2),
      };

    case 'bottom':
      return {
        top: element.top + element.height + 5,
        left: (element.left + (element.width / 2)) - (tooltip.width / 2),
      };

    case 'left':
      return {
        top: (element.top + (element.height / 2)) - (tooltip.height / 2),
        left: element.left - tooltip.width - 5,
      };

    default:
      return {
        top: (element.top + (element.height / 2)) - (tooltip.height / 2),
        left: element.left + element.width + 5,
      };
  }
};

const getBestPosition = (style, position, tooltip) => {
  switch (position) {
    case 'top':
      if (style.top - tooltip.height < 20) {
        return 'bottom';
      }

      break;

    case 'bottom':
      if (style.top + tooltip.height > window.innerHeight - 20) {
        return 'top';
      }

      break;

    case 'left':
      if (style.left < 20) {
        return 'right';
      }
      break;

    default:
      if (style.left + tooltip.width > window.innerWidth - 20) {
        return 'left';
      }
      break;
  }

  return position;
};

const getBestStyle = (style, tooltip) => {
  const newStyle = { ...style };

  if (newStyle.left < 5) {
    newStyle.left = 5;
  }

  if (newStyle.left + tooltip.width > window.innerWidth - 5) {
    newStyle.left = window.innerWidth - (tooltip.width + 5);
  }

  return newStyle;
};

export const getWrapperBestPosition = (element, tooltip, defaultPosition) => {
  const elementRect = element.getBoundingClientRect();
  const tooltipRect = tooltip.getBoundingClientRect();

  const style = getStylePosition(elementRect, tooltipRect, defaultPosition);
  const newPosition = getBestPosition(style, defaultPosition, tooltipRect);

  if (newPosition !== defaultPosition) {
    return {
      position: newPosition,
      style: getBestStyle(getStylePosition(elementRect, tooltipRect, newPosition), tooltipRect),
    };
  }

  return {
    position: defaultPosition,
    style: getBestStyle(style, tooltipRect),
  };
};
