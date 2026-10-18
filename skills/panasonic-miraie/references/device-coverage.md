# Device coverage and extension boundaries

Only one Panasonic AC model, `CS-CU-NU18BKY5WX` / model `140013`, was exercised against a real account and appliance in this investigation. All other families below are **static declarations in MirAIe Android 01.4.32**. Their presence establishes useful implementation leads, not current account availability, tested commands, sensor accuracy, or a complete integration.

For the tested AC's current vertical/horizontal swing coverage, use
[swing.md](swing.md). Codes have cloud readback evidence and code-specific owner
observations; mappings remain limited to one unit, with unknown firmware and exact
angles. This does not promote other families or all advertised capabilities to
live-verified support.

## Families found in the shipped app

Model capability names below are exact registry strings. They are not status/control wire keys, and they do not guarantee that every member of the family advertises them. Fetch each actual model before mapping controls.

| Family | Shipped classes / categories | Declared capability examples |
|---|---|---|
| Air conditioners | `ACDevice`; AC, CAC, MAC, RAC, TAC; separate Intesis and Comfort Cloud category identifiers | `ACMode`, `ACTemperature`, `ACFanSpeed`, `ACModeTemperature`, `Convertible` |
| Fans | `FanDevice`, `ANCHORFAN`, fan controllers and virtual fans | `FanMode`, `FanSpeed`, `RoomTemperature`, `RoomHumidity`, `FanTimer` |
| Refrigerators | `RefrigeratorDevice`, `REFRIGERATOR` | `RefMode`, `Compartments`, `Fridge-Temperature`, `Freezer-Temperature`, `ambientTemperature`, `FridgeDoorStatus`, `FreezerDoorStatus`, `ChildLock`, `ConvertPrimefreshModes`, `PrimefreshModes`, `QuickFreeze`, `FaultStatus` |
| Washing machines | `WashingMachineDevice`, `WASHING_MACHINE`, `TL_WASHING_MACHINE` | `WashActions`, `WashActionTypes`, `WashStatus`, `WashPrograms`, `RemainingTime`, `TotalTime`, `DelayTime`, `CurrentCycle`, `WaterTemperature`, `SpinSpeed`, `RinseCycle`, `WashDuration`, `SpinDuration`, `WaterLevel`, `Faults` |
| Water heaters | `WaterHeaterDevice`, `WATER_HEATER` | `WHMode`, `WHTargetTemperature`, `WHCurrentTemperature`, `WHHeatingState`, `WHTimers`, `WHTimerState`, `WHAutoPowerOff`, `WHHeaterFaultStatus` |
| TVs | `TVDevice`, `TV` | `Volume`, `Mute`, `TVInputSource`, `LaunchApps`, `TVFaults` |
| Switches, plugs, dimmers, curtains and scene controllers | Anchor devices, Wi-Fi switches/plugs, Roma, Smart Touch, Spike Guard, Vetaar gateway and virtual subdevices | `ChannelPowerStatus`, `ChannelNumber`, `DimmerState`, `CurtainControl`, `CurtainLevel`, `TargetCurtainLevel`, `CalibrationState`, `CalibrateCurtain`, `groupCommand`, `udpControl`, `TwoWaySwitch` |
| LED lights | `LEDDevice`, `LED_LIGHTS` and light virtual devices | `Brightness`, `ColourTemperature`, `RGBColors`, `FadeTime`, `RiseTime`, `mode`, `PresetScenes` |
| Cameras | `CameraDevice`, `CAMERA` | `Resolution`, `FrameRate`, `BitRate`, `LocalRecordingStatus`, `MotionAlert`, `TamperAlert` |
| Video door phones | `VDPDevice`, `VDP` | `DetectionSensitivity`, `ImageFlip`, `ActiveTime`, `SceneMode`, `CloudVideo`, `LEDIndicator`, `VideoResolution` |
| Air-quality sensors | `UhooDevice`, `Uhoo` | `Room Temperature`, `PM2.5`, `Humidity`, `TVOC`, `Carbon Monoxide`, `Carbon Dioxide`, `Nitrogen Dioxide`, `Ozone`, `Air Pressure`, `Virus Index` |
| IR blasters and virtual remotes | `IRRemoteDevice`; virtual AC, fan, light, soundbar, STB, TV and other remote identifiers | Supported device categories; specialized IR AC, TV channel/digits/volume/input/playback/keypad/apps, custom keys and fan speed capabilities |
| Other integrations | `OhmDevice`, `OHM_SUSTLAB`; `MatterDevice`, `MATTER`; third-party service link/sync paths | Separate services, schemas and commissioning paths require investigation; generic AC REST commands are not established for them |

These are category/adapter families, not a list of all currently sold Panasonic products. Category enum names such as `WATER_HEATER` are Java identifiers; do not assume they equal a server's category string. Video resolution capabilities do not establish a streaming or media-authentication protocol. IR state may represent a remote command rather than observed appliance state; verify that distinction per adapter.

## Parent, child and service routing

The shared model schema carries `category`, `subCategory`, `thirdPartyService` and virtual `devices`. `BaseDevice` separately exposes parent device ID, subdevices, virtual devices, channel number, local address, UDP encryption details and MQTT topics. Preserve these relationships rather than flattening all children into independent Wi-Fi appliances.

The home discovery schema has home `metaTopic` and a device `topic` **list**. Topic strings are account-specific data. Read the returned schema and trace the selected adapter's topic methods before subscribing or publishing; do not build topics by concatenating guessed IDs.

The APK declares Anchor `auth`, `tokenRefresh` and `DeviceControl3` routes as well as MirAIe service-linking paths. This does not prove that Anchor credentials or control syntax match MirAIe. Similarly, Intesis and Panasonic Comfort Cloud entries are leads for separate adapters, not evidence their protocols use the MirAIe India AC envelope. Matter commissioning, LAN/UDP, BLE, gateway subdevices, camera/VDP SDKs and IR remotes need their own protocol evidence.

## Shared route inventory

These paths are exact Retrofit declarations or the established AC control path. Resolve the appropriate base URL and service first. A declaration alone does not establish body/query fields, permissions, current server success or compatibility with a selected family. See the route catalog and serialized schemas for the available static details.

| Operation | Method and path | Coverage |
|---|---|---|
| Discover hierarchy | `GET homeManagement/homes` | Live verified for the tested AC account |
| Model lookup | `GET deviceManagement/deviceModels/modelId/{modelId}` | Live verified for the tested AC |
| Model-number lookup | `GET deviceManagement/deviceModels/modelNumber/{modelNumber}` | Static only |
| Device status | `GET deviceManagement/devices/{deviceIdentifier}/mobile/status` | Live verified using the tested device ID |
| Control | `POST deviceManagement/control` | Live verified for documented AC operations; family-specific payload and routing required elsewhere |
| Device configuration | `GET` / `PUT deviceservices/device/{deviceId}/config` | Static only |
| Power / water history | `GET powerConsumption/devices/{deviceId}` / `GET waterConsumption/devices/{deviceId}` | Power history live verified for the AC; water history static only |
| Device name / virtual appliance mapping | `PUT homeManagement/homes/{homeId}/spaces/{spaceId}/devices/{deviceId}` | Static only; distinct request bodies share this path |
| Move between spaces | `POST homeManagement/homes/{homeId}/spaces/{spaceId}/devices/{deviceId}` | Static only |
| Offboard | `POST deviceManagement/devices/{deviceId}/deviceOffboard` | Static only; account/device mutation, not an ordinary discovery step |

Additional static route families include home/space CRUD; `actionGroups` CRUD and `/activate`; `rms/homes/{homeId}/rules` CRUD and `/pause` / `/resume`; washing-machine `washProgram`, `washWizard`, `washHistory` and pre-treatment; IR virtual-device/custom-remote creation; VDP registration/files; Matter fabric controller/node NOC and device onboarding. Partner routes include `appsupport/services/serviceLinkStatus` and `deviceManagement/services/linkService`, `syncService`, `unlinkService`; some declarations use absolute `/simplifi/v1/appsupport/services/...` paths including `relinkService`. Preserve those path differences rather than mechanically joining every path to the same base. Bodies and lifecycle behavior remain to be established for these operations.

## Capability registry cautions

[capability-registry.json](capability-registry.json) contains 231 exact constructor records from the shipped `BaseCapability.Type` registry: enum identifier, model capability name, implementing class and `ki` value. It includes source identity, extraction checks and collisions. It is a static reference, not a universal command map.

The enum identifier can differ from the model capability name: `RFMode` maps to `RefMode`; `WaterHeaterMode` maps to `WHMode`. Neither is the actual model attribute key. `ki` is a constructor argument, not an enum ordinal or an array index. It is not globally unique: `TotalTime` and `DelayTime` both have 78; supported IR device categories and LED fade time both have 290. The model capability name `TimeStamp` also has two registry entries, with indexes 103 and 105. Resolve duplicates using the actual model section, implementing class, adapter and native command builder. Do not add `ki` to arbitrary writes merely because a registry entry exists; an entry can be status/config metadata rather than a writable command.

## What another device needs

Without hardware, an agent can implement shared authentication/discovery, model parsing, a read-only inspector, telemetry storage and statically established builders with synthetic fixtures. To claim a new family/model works, obtain:

1. An authorized account containing the device, plus sanitized hierarchy, model and status responses. Record exact app version, model, firmware and service/parent/channel relationships.
2. The applicable native adapter and command builder evidence, including wire types/enums, extra parameters, indexes, dependencies and transport routing. Do not transplant AC `cnt`, `sid`, keys or mode rules without evidence.
3. Relevant hardware or a cooperating owner for a reversible control test, physical/result confirmation and restoration of baseline. Readback alone may be an echoed request.
4. For metrics, real samples establishing units, missing-value rules, timestamp meaning, update cadence and whether readings are measured or derived. Static sensor names do not establish their presence or accuracy.
5. For onboarding or alternative transports, the actual commissioning/pairing mechanism, required SDKs or framing, credential lifecycle, and supported hardware. A NOC route, UDP key field or camera capability alone is insufficient.

The local APK evidence establishes substantial breadth for future work. Completing every family without access to representative devices cannot honestly establish end-to-end support.

## Provenance

- Artifact: `com.panasonic.in.miraie` 01.4.32, SHA-256 `62a36f534b6e38802e9a1ff2138a6342d1328e0f0a4928ce37ba2299e2f867a1`.
- Capability registry initializer: REA `ev_8afca330c2976c1b28c0b48c4a64b557da53a9bc7a63946625bc2d5f2903e071` (complete smali static initializer).
- Device schema and categories: extracted DEX metadata/serialized annotations for `EDeviceType`, `DeviceModelDetail`, `UserHome`, and the listed device/capability classes.
- Generic device class: REA `ev_f6b4ae84f341a3d631637e8803490edafa3669301803ea054183e5b86bf2472b` (`BaseDevice` class inventory).
- Generic control dispatch: REA `ev_85b1ca1e706815c3bb77e21f3649efcd148c3310c735df8c83c7932b5338112d`; `sendControlMessage` posts a home ID, adapter control topic and supplied JSON to the MQTT event bus. This is static dispatch evidence, not a verified universal payload.
- Route inventory: Retrofit method annotations extracted from the same artifact. Static decompilation and metadata do not establish execution.
