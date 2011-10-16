package com.fasterxml.jackson.module.scala

import deser.SeqDeserializerModule
import ser.IterableSerializerModule

trait SeqModule extends IterableSerializerModule with SeqDeserializerModule {
}