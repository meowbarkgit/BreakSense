# Break Sense

A Fabric client-side mod that adds DualSense adaptive trigger support while mining blocks - powered by [Controlify](https://github.com/isXander/Controlify).

Trigger intensity and feedback type vary based on block material, hardness, tool used, and active effects (Haste / Mining Fatigue).

## Compatibility

| Dependency | Version  |
|---|----------|
| Minecraft | ~26.1.x  |
| Fabric Loader | ≥ 0.19.2 |
| Controlify | ≥ 3.0.0  |

Client-side only.

## Trigger Behavior

**Stone** - pulsing vibration, frequency tied to mining progress  
**Wood** - vibration that grows stronger as the block breaks  
**Dirt / Gravel / Sand** - soft, low-resistance feedback  
**Metal** - sharp, stiff feedback effect  
**Glass** - quick high-frequency burst  
**Wool** - barely-there feedback, almost no resistance  
**Pickaxe** - overrides the material effect with its own pulse pattern

On block break - a short impact impulse fires regardless of material.

## Building

```bash
./gradlew build
```

Output jar will be in `build/libs/`.

## Author

**Recode** - MIT License