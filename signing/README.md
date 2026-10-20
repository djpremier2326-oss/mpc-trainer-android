# Training-only debug signing

`training-debug.keystore.b64` is a public, development-only Android debug key.
Its standard password and alias are intentionally not secrets. Never use it for production releases or store distribution.

The separate educational package uses this same key across builds so Android can install subsequent training updates without changing the certificate. The old trainer package remains installed independently.
