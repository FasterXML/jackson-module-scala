package com.fasterxml.jackson.module

/**
 * Provides Scala support for the [[http://jackson.codehaus.org Jackson JSON Processor]].
 *
 * Currently, full support is provided for:
 *  - Option
 *  - Tuple
 *  - Seq
 *  - Enumeration
 *
 *  Partial support exists for:
 *  - Map; all maps are serialized but only unsorted maps can currently be deserialized.
 *  - Iterable; any iterable can be serialized but cannot be deserialized.
 *
 * @example {{{
 * def mapper = new ObjectMapper()
 * mapper.registerModule(DefaultScalaModule)
 * }}}
 *
 * @author Christopher Currie <christopher@currie.com>
 * @since 1.9.0
 */
package object scala