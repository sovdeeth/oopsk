# oopsk

oopsk is a Skript addon that aims to add limited object-oriented programming tools to Skript in a non-invasive manner. 

## Basics
The current feature set revolves around `structs`, simple objects that group together a set of typed fields. Structs are defined by struct templates, top-level structures that define their name and the fields they contain:
```
struct Message:
  contents: string
  sender: offline player
  timestamp: date = now
```
A template's name and field names are case-insensitive. The template name follows the same rules as a function name, while field names can only consist of letters, underscores, and spaces. 
Each field has a name and a type, as well as an optional default value. This default value may be an expression, as it is evaluated when a struct is created, not when the template is registered.

Creating a struct involves a simple expression:
```
set {_a} to a message struct
```
`{_a}` is now a new instance of the Message template, with contents and sender unset and timestamp set to the date the struct was created. Fields can be accessed and modified with the field access expression:
```
set field contents of {_a} to "hello world"
broadcast {_a}'s sender field
reset {_a}->timestamp
```
### Type Safety
oopsk attempts to ensure type safety at parse time via checking all fields with the given name. This means having unique field names across structs allows oopsk to give you more accurate parse errors, while sharing field names can result in invalid code not causing any errors during parsing. 
Any type violations not caught during parsing should be caught at runtime via runtime errors that cannot be suppressed. Note that code parsed in one script prior to updates to a struct in another script will not show parse errors until it is reloaded again, though it should properly emit runtime errors.

### Modifying the Template of Existing Structs
Any modifications to a template will be reflected in all structs that have been previously created from that template. This can result in data loss if a field is renamed, removed, or changes type, as existing structs will have their modified fields either removed or re-evaluated as appropriate. 
Note that this means default values need to be re-evaluated and therefore will not have been evaluated when the struct was created. oopsk will print a warning in console if any existing structs were modified as a result of template changes. Adding fields to a template or changing default values will not modify existing structs.

Removing a template will 'orphan' existing structs, who will function as if the template still existed, though it may be impossible to access their information as the field access expression may not recognize the field names. If a new template is added with the same name, these existing structs will be updated to match the new template.
If you remove a template, you should take care to remove structs that depended on it.

**I recommend putting your templates in a separate script file, so you can limit the chances of them being accidentally modified or disabled.**

## Roadmap
Alpha 2:
- `constant` modifier for fields
- condition to test what template a struct was created from
- Expression Section for creating structs

Beta:
- Expression to get all field names of a struct
- Expression to dynamically access a field from a string (unsafe)
- Serialization of structs (unsure on feasibility)

Possibilities:
- Methods?
- Allowing fields to accept structs from specific templates, rather than any struct.

## Docs
### Struct Template
```
struct <name>:
  <field name>: <field type>
  <field name>: <field type> = <default value>
```
Registers a new template with the given name and fields. Both template names and field names are case-insensitive.
- <name>: The template's name. Follows the restrictions of a function name.
- <field name>: The name of a field. Consists of letters, underscores, and spaces.
- <field type>: A Skript type that determines the type of the field. Can be plural for a list of values.
- <default value>: Optional. An expression that will be evaluated when a struct is created from this template.

### Create a Struct
```
[a[n]] <template name> struct [instance]
```
Creates a struct instance from the provided template. Default values are evaluated and assigned. 

### Access a Field
```
[the] field <field name> [of] %struct%
%struct%'[s] <field name> field
%struct%[ ]->[ ]<field name>
```
Gets the field of a given struct. Fields can be set, deleted, or reset to their default values (this re-evaluates the default value expression).

