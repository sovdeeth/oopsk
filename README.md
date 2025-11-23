# oopsk

oopsk is a Skript addon that aims to add limited object-oriented programming tools to Skript in a non-invasive manner. 

## Basics
The current feature set revolves around `structs`, simple objects that group together a set of typed fields. Structs are defined by struct templates, top-level structures that define their name and the fields they contain:
```
struct Message:
  contents: string
  sender: offline player
  const timestamp: date = now
  converts to:
    string via this->contents
```
A template's name and field names are case-insensitive. The template name follows the same rules as a function name, while field names can only consist of letters, underscores, and spaces. 
Each field has a name and a type, as well as an optional default value. This default value may be an expression, as it is evaluated when a struct is created, not when the template is registered.
Adding `const` or `constant` to the start will prevent the field from being changed after creation.

Structs also support conversion expressions, which allow you to define how a struct can be converted to other types. The syntax is 
`<type> via <expression>`, where `<type>` is the type to convert to and `<expression>` is an expression that evaluates to that type. 
The expression may use `this` to refer to the struct being converted. Multiple conversions can be defined by adding more lines under the `converts to:` section.

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
Structs can also be created with initial values:
```
set {_a} to a message struct:
  sender: player
  contents: "hello world"
```

### Custom Types

Struct templates automatically create a new Skript type using the template's name + `struct`. 
In the above example, `message struct` is now a valid Skript type that can be used in function parameters and other struct fields.
**You should be very careful when reloading templates, as any existing code that used the type may break if the template was modified or removed.**
ALWAYS reload all scripts after modifying templates to ensure all code is properly re-parsed.

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
Beta:
- Reflective expressions for structs and field (get fields, get types, get whether a field is constant...)
- Expression to dynamically access a field from a string (unsafe)
- Serialization of structs (unsure on feasibility)

Possibilities:
- Methods?
- Allowing fields to accept structs from specific templates, rather than any struct.

## Docs

Docs are available on [skUnity](https://docs.skunity.com/syntax/search/addon:oopsk) or on [SkriptHub](http://skripthub.net/docs/?addon=oopsk).

