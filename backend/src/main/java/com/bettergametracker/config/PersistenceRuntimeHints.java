package com.bettergametracker.config;

import com.bettergametracker.play.PlayEntrySummary;
import org.hibernate.community.dialect.SQLiteDialect;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class PersistenceRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(SQLiteDialect.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.reflection().registerType(PlayEntrySummary.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.resources().registerPattern("db/migration/**");
    }
}
