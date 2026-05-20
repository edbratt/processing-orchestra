# Using and Converting Sketches

This project supports two different sketch workflows:

1. run one of the included Java sketch classes
2. convert or adapt an existing Processing PDE sketch into the server environment

## Included Sketches

The current in-repo sketch list is maintained in [Samples Index](samples-index.md).

Those are the sketches that should be treated as project material today. External sketches you may be reviewing elsewhere should stay outside that list until they are actually brought into the repository.

## Conversion and Adaptation

There are two common paths:

- mechanical conversion with the PDE converter
- manual adaptation when the sketch needs a new multi-user interaction model

The converter is useful when a sketch mostly needs:

- Java generation from PDE
- helper class preservation
- `size(..., renderer)` preservation
- browser-touch or browser-keyboard style mapping

Manual adaptation is usually better when a sketch needs:

- one graphic per client
- motion-driven behavior
- per-user color or palette ownership
- a different interaction model than the original mouse logic

## Related References

- [Abandoned Art Converter Review](abandoned-art-converter-review.md)
- [PDE Converter Status](../work-in-progress/PDE_CONVERTER_STATUS.md)
- [PDE to Processing Sketch Spec](../work-in-progress/PDE_TO_PROCESSING_SKETCH_SPEC.md)
