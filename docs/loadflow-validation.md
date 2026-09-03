---
layout: default
---

# Validate load flow results

This tutorial shows how to validate load flow results with PowSyBl. It introduces the available validation types and illustrates bus validation before and after running a load flow.

## What will you build?

You will load a network, run bus validation, compute a load flow, and validate the resulting active and reactive power balances.

PowSyBl provides the following validation types:

- `BUSES`
- `FLOWS`
- `GENERATORS`
- `SVCS`
- `SHUNTS`
- `TWTS`
- `TWTS3W`

## What will you need?

- A favorite text editor or IDE
- JDK 21 or later
- Maven 3.9 or later
- The example network provided with this tutorial

## How to complete this tutorial?

You can follow the steps below to understand how load flow validation works. You can also download or clone the completed sources from the [PowSyBl tutorials repository](https://github.com/powsybl/powsybl-tutorials).

## Configure PowSyBl
We have configured this tutorial to use a locally defined config.yml file

```yaml
load-flow:
  default-impl-name: "OpenLoadFlow"
table-formatter:
  invalid-string: NOT_CALCULATED
```

## Inspect the network

The tutorial uses the following network:

![Network area diagram](./img/loadflow-validation/network_nad.svg){width="75%" .center-image}

The example focuses on the bus connecting generator `GEN` to the two-winding transformer `NGEN_NHV1`. Its calculated bus identifier is `VLGEN_0`.

## Validate a bus

Bus validation checks Kirchhoff's active and reactive power balance equations:

$$
\begin{align*}
|\mathrm{incomingP} + \mathrm{loadP}| &\leq \mathrm{threshold} \\
|\mathrm{incomingQ} + \mathrm{loadQ}| &\leq \mathrm{threshold}
\end{align*}
$$

The following diagram highlights the equipment involved in the validation:

![Bus validation example](./img/loadflow-validation/bus_validation_example.jpeg){width="75%" .center-image}

### Validate before running the load flow

Before running the load flow, terminal power values are missing. The CSV formatter displays them as `NOT_CALCULATED`:

```text
VLGEN_0;incomingP;NOT_CALCULATED
VLGEN_0;incomingQ;NOT_CALCULATED
```

### Run the load flow

After the load flow, the active power values at the generator bus are:

```text
Generator terminal P:    -605.5595954348781 MW
Transformer terminal P:  +605.5595848046546 MW
```

### Validate the results

The resulting active power balance is:

```text
incomingP = generatorP + transformerP
          = -605.5595954348781 + 605.5595848046546
          = -1.06302235e-05 MW
```

The CSV formatter displays:

```text
VLGEN_0;incomingP;-1.06302e-05
VLGEN_0;incomingQ;0.00000
```

No load is connected directly to this bus:

```text
loadP = 0 MW
```

The residual is not exactly zero because of numerical precision. A non-zero validation threshold is therefore required to accept it.

Add the following section to your PowSyBl configuration file:

```yaml
loadflow-validation:
  threshold: 0.01
```