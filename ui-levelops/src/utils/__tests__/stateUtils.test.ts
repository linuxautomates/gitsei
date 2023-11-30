import { restAPILoadingState } from '../stateUtil'; 

describe('restAPILoadingState Function', () => {
  it('should return loading as true and error as false when id is found in the state', () => {
    const restAPIState = {
      "0": {
        loading: true,
        error: false
      }
    };

    const result = restAPILoadingState(restAPIState, "0");

    expect(result).toEqual({ loading: true, error: false });
  });

  it('should return loading as true and error as false when id is not found in the state', () => {
    const restAPIState = {
      "1": {
        loading: true,
        error: false
      }
    };

    const result = restAPILoadingState(restAPIState, "0");

    expect(result).toEqual({ loading: true, error: false });
  });

  it('should handle missing id parameter and default to "0"', () => {
    const restAPIState = {
      "0": {
        loading: false,
        error: true
      }
    };

    const result = restAPILoadingState(restAPIState,"0");

    expect(result).toEqual({ loading: false, error: true });
  });

  
  it('should handle a null or undefined state parameter and return the default loading and error values', () => {
    const result = restAPILoadingState(null, "0");

    expect(result).toEqual({ loading: true, error: false });
  });
});
