// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model.source

const val VUE_NAMESPACE: String = "Vue"

const val EXTEND_FUN: String = "extend"
const val COMPONENT_FUN: String = "component"
const val MIXIN_FUN: String = "mixin"
const val DIRECTIVE_FUN: String = "directive"
const val FILTER_FUN: String = "filter"
const val DEFINE_COMPONENT_FUN = "defineComponent"
const val DEFINE_NUXT_COMPONENT_FUN = "defineNuxtComponent" // so far, the IDE treats it just as an alias for defineComponent
const val DEFINE_PROPS_FUN = "defineProps"
const val DEFINE_EMITS_FUN = "defineEmits"
const val DEFINE_EXPOSE_FUN = "defineExpose"
const val WITH_DEFAULTS_FUN = "withDefaults"
const val CREATE_APP_FUN = "createApp"
const val MOUNT_FUN = "mount"

const val MIXINS_PROP: String = "mixins"
const val EXTENDS_PROP: String = "extends"
const val DIRECTIVES_PROP: String = "directives"
const val COMPONENTS_PROP: String = "components"
const val FILTERS_PROP: String = "filters"
const val SETUP_METHOD: String = "setup"
const val NAME_PROP: String = "name"
const val TEMPLATE_PROP: String = "template"
const val METHODS_PROP = "methods"
const val EMITS_PROP = "emits"
const val COMPUTED_PROP = "computed"
const val WATCH_PROP = "watch"
const val DATA_PROP = "data"
const val MODEL_PROP = "model"
const val MODEL_PROP_PROP = "prop"
const val MODEL_EVENT_PROP = "event"
const val DELIMITERS_PROP = "delimiters"
const val PROPS_PROP = "props"
const val PROPS_TYPE_PROP = "type"
const val PROPS_REQUIRED_PROP = "required"
const val PROPS_DEFAULT_PROP = "default"
const val EL_PROP = "el"

const val INSTANCE_PROPS_PROP = "\$props"
const val INSTANCE_REFS_PROP = "\$refs"
const val INSTANCE_SLOTS_PROP = "\$slots"
const val INSTANCE_EMIT_METHOD = "\$emit"
const val INSTANCE_DATA_PROP = "\$data"
const val INSTANCE_OPTIONS_PROP = "\$options"
